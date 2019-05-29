package wtf.brzhk.simpleapp.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.cloud.kubernetes.PodUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {
    private PodUtils podUtils;

    public MetricsConfig(PodUtils podUtils) {
        this.podUtils = podUtils;
    }

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
                .commonTags("application", "simpleapp",
                        "pod_name", podUtils.currentPod().get().getMetadata().getName());
    }

}