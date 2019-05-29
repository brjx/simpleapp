package wtf.brzhk.simpleapp;

import io.fabric8.kubernetes.api.model.Pod;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.kubernetes.PodUtils;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableMap;

@SpringBootApplication
@Controller
@EnableScheduling
@EnableDiscoveryClient
@Timed()
public class DemoApplication {

    private final MeterRegistry registry;
    private DiscoveryClient discoveryClient;
    private PodUtils podUtils;

    public DemoApplication(MeterRegistry registry, DiscoveryClient discoveryClient, PodUtils podUtils) {
        this.registry = registry;
        this.podUtils = podUtils;
        this.discoveryClient = discoveryClient;
    }

    @GetMapping(path = "/", produces = "application/json")
    @ResponseBody
    public Map<String, Object> landingPage() {
        Counter.builder("mymetric").tag("foo", "bar").register(registry).increment();
        Pod current = this.podUtils.currentPod().get();
        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        if (current != null) {
            details.put("inside", true);
            details.put("namespace", current.getMetadata().getNamespace());
            details.put("podName", current.getMetadata().getName());
            details.put("podIp", current.getStatus().getPodIP());
            details.put("serviceAccount", current.getSpec().getServiceAccountName());
            details.put("nodeName", current.getSpec().getNodeName());
            details.put("hostIp", current.getStatus().getHostIP());
            current.getMetadata().getAnnotations()
                    .entrySet().forEach(stringStringEntry -> {
                details.put("annotation: " + stringStringEntry.getKey(),
                        stringStringEntry.getValue());
            });
            current.getMetadata().getLabels()
                    .entrySet().forEach(stringStringEntry -> {
                details.put("label: " + stringStringEntry.getKey(), stringStringEntry.getValue());
            });
        } else {
            details.put("inside", false);
        }
        return unmodifiableMap(details);
    }

    @GetMapping(path = "/services", produces = "application/json")
    @ResponseBody
    public Map<String, String> services() {
        return discoveryClient.getServices().stream().map(s -> discoveryClient.getInstances(s)).flatMap(Collection::stream)
                .collect(Collectors.toList()).stream().collect(Collectors.toMap(o -> o.getUri().toString(), ServiceInstance::getServiceId));
    }

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
