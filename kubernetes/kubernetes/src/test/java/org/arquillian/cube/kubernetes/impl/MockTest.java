package org.arquillian.cube.kubernetes.impl;

import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodListBuilder;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerListBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceListBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.server.mock.KubernetesMockServer;
import org.arquillian.cube.kubernetes.impl.requirement.RequiresKubernetes;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.IOException;

@RunWith(Suite.class)
@Suite.SuiteClasses(
        {
                PodInjection.class,
                ReplicationControllerInjection.class,
                ServiceInjection.class,
        }
)
@RequiresKubernetes
public class MockTest {

    private static final KubernetesMockServer MOCK = new KubernetesMockServer();

    @BeforeClass
    public static void setUpClass() throws IOException {

        Pod testPod = new PodBuilder()
                .withNewMetadata()
                    .withName("test-pod")
                .endMetadata()
                .withNewStatus()
                    .withPhase("Running")
                .endStatus()
                .build();

        Service testService = new ServiceBuilder()
                .withNewMetadata()
                    .withName("test-service")
                .endMetadata()
                .withNewSpec()
                    .withClusterIP("10.0.0.1")
                    .addNewPort()
                        .withPort(8080)
                    .endPort()
                .endSpec()
                .build();

        ReplicationController testController = new ReplicationControllerBuilder()
                .withNewMetadata()
                    .withName("test-controller")
                .endMetadata()
                .withNewSpec()
                .addToSelector("name", "somelabel")
                    .withReplicas(1)
                    .withNewTemplate()
                        .withNewMetadata()
                            .addToLabels("name","somelabel")
                        .endMetadata()
                        .withNewSpec()
                            .addNewContainer()
                                .withName("test-container2")
                                .withImage("test/image2")
                            .endContainer()
                        .endSpec()
                    .endTemplate()
                .endSpec()
                .build();


        MOCK.expect().get().withPath("/api/v1/namespaces/arquillian").andReturn(200, new NamespaceBuilder()
                .withNewMetadata()
                .withName("arquillian")
                .and().build()).always();


        //test-controller
        MOCK.expect().post().withPath("/api/v1/namespaces/arquillian/replicationcontrollers").andReturn(201, testController).always();
        MOCK.expect().get().withPath("/api/v1/namespaces/arquillian/replicationcontrollers/test-controller").andReturn(404, "").once();
        MOCK.expect().get().withPath("/api/v1/namespaces/arquillian/replicationcontrollers/test-controller").andReturn(200, testController).always();
        MOCK.expect().get().withPath("/api/v1/namespaces/arquillian/replicationcontrollers").andReturn(200, new ReplicationControllerListBuilder()
                .withItems(testController).build())
                .always();
        MOCK.expect().delete().withPath("/api/v1/namespaces/arquillian/replicationcontrollers/test-controller").andReturn(200, "").always();

        //test-pod
        MOCK.expect().post().withPath("/api/v1/namespaces/arquillian/pods").andReturn(201, testPod).always();
        MOCK.expect().get().withPath("/api/v1/namespaces/arquillian/pods/test-pod").andReturn(404, "").once();
        MOCK.expect().get().withPath("/api/v1/namespaces/arquillian/pods/test-pod").andReturn(200, testPod).always();
        MOCK.expect().get().withPath("/api/v1/namespaces/arquillian/pods").andReturn(200, new PodListBuilder()
                .withItems(testPod)
                .build()).always();
        MOCK.expect().delete().withPath("/api/v1/namespaces/arquillian/pods/test-pod").andReturn(200, "").always();

        //test-service
        MOCK.expect().post().withPath("/api/v1/namespaces/arquillian/services").andReturn(201, testService).always();
        MOCK.expect().get().withPath("/api/v1/namespaces/arquillian/services/test-service").andReturn(404, "").once();
        MOCK.expect().get().withPath("/api/v1/namespaces/arquillian/services/test-service").andReturn(200, testService).always();
        MOCK.expect().get().withPath("/api/v1/namespaces/arquillian/services").andReturn(200, new ServiceListBuilder()
                .withItems(testService)
                .build()).always();
        MOCK.expect().delete().withPath("/api/v1/namespaces/arquillian/services/test-service").andReturn(200, "").always();

        MOCK.init();

        String masterUrl = MOCK.getServer().url("/").toString();
        System.setProperty(Config.KUBERNETES_MASTER_SYSTEM_PROPERTY, masterUrl);
        System.setProperty(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true");
        System.setProperty(Config.KUBERNETES_NAMESPACE_SYSTEM_PROPERTY, "arquillian");
        System.setProperty(Constants.NAMESPACE_TO_USE, "arquillian");
        System.setProperty(Constants.NAMESPACE_CLEANUP_ENABLED, "true");
        System.setProperty(Constants.ENVIRONMENT_CONFIG_URL, MockTest.class.getResource("/test-kubernetes-1.json").toString());
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        //MOCK.destroy();
    }
}
