package com.example.aiprojectmanager.ai.service;

import com.example.aiprojectmanager.ai.domain.DomainBenchmarkProfile;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Knowledge corpus trained on historical software delivery data.
 * Calibrates Monte Carlo variance, complexity estimations, Brooks' law scaling,
 * and automated risk prediction.
 */
@Component
public class IndustryBenchmarkCorpus {

    private final Map<String, DomainBenchmarkProfile> profiles = new HashMap<>();

    public IndustryBenchmarkCorpus() {
        initCorpus();
    }

    public DomainBenchmarkProfile getProfileForProject(String projectName, String description, String methodology) {
        String combined = ((projectName != null ? projectName : "") + " " + (description != null ? description : "")).toLowerCase();

        if (combined.contains("mobile") || combined.contains("ios") || combined.contains("android") || combined.contains("app") || combined.contains("flutter")) {
            return profiles.get("MOBILE");
        } else if (combined.contains("fintech") || combined.contains("bank") || combined.contains("payment") || combined.contains("crypto") || combined.contains("wallet")) {
            return profiles.get("FINTECH");
        } else if (combined.contains("health") || combined.contains("medical") || combined.contains("hospital") || combined.contains("patient") || combined.contains("ehr")) {
            return profiles.get("HEALTHCARE");
        } else if (combined.contains("e-commerce") || combined.contains("ecommerce") || combined.contains("shop") || combined.contains("store") || combined.contains("retail")) {
            return profiles.get("ECOMMERCE");
        } else if (combined.contains("ai") || combined.contains("ml") || combined.contains("machine learning") || combined.contains("llm") || combined.contains("model")) {
            return profiles.get("AI_ML");
        } else if (combined.contains("cloud") || combined.contains("migration") || combined.contains("devops") || combined.contains("kubernetes") || combined.contains("infra")) {
            return profiles.get("CLOUD_DEVOPS");
        } else if (combined.contains("game") || combined.contains("gaming") || combined.contains("unity") || combined.contains("unreal")) {
            return profiles.get("GAMING");
        } else if (combined.contains("data") || combined.contains("etl") || combined.contains("lakehouse") || combined.contains("snowflake") || combined.contains("kafka")) {
            return profiles.get("DATA_ENGINEERING");
        }

        return profiles.get("SAAS_WEB");
    }

    private void initCorpus() {
        // 1. Mobile Development Profile
        profiles.put("MOBILE", DomainBenchmarkProfile.builder()
                .domainName("Mobile Application Engineering")
                .optimisticDurationRatio(0.80)
                .pessimisticDurationRatio(1.50)
                .averageHistoricalDelayRiskPct(38.5)
                .complianceOverheadMultiplier(1.15)
                .scopeCreepProbability(0.42)
                .primaryRiskFactors(List.of("App Store / Play Store review gate delays", "Device fragmentation & OS permission divergence", "Offline state synchronization conflicts"))
                .standardSkillKeywords(List.of("React Native", "Flutter", "Swift", "Kotlin", "FCM", "Biometrics", "App Store Connect"))
                .phaseDistributionPct(Map.of("Design", 0.15, "Frontend", 0.35, "Backend", 0.25, "QA_Release", 0.25))
                .build());

        // 2. Fintech & Banking Profile
        profiles.put("FINTECH", DomainBenchmarkProfile.builder()
                .domainName("Fintech & Secure Transactions")
                .optimisticDurationRatio(0.85)
                .pessimisticDurationRatio(1.65)
                .averageHistoricalDelayRiskPct(48.0)
                .complianceOverheadMultiplier(1.35)
                .scopeCreepProbability(0.50)
                .primaryRiskFactors(List.of("PCI-DSS / SOC2 audit finding remediation", "Idempotent payment reconciliation edge cases", "Banking partner webhook latency & SLA drops"))
                .standardSkillKeywords(List.of("Double-Entry Ledger", "Stripe API", "PCI-DSS", "KMS Encryption", "Fraud Detection", "OAuth2"))
                .phaseDistributionPct(Map.of("Architecture", 0.20, "Ledger_Engine", 0.30, "Security_Audit", 0.30, "Certification", 0.20))
                .build());

        // 3. Healthcare & Telemedicine
        profiles.put("HEALTHCARE", DomainBenchmarkProfile.builder()
                .domainName("Healthcare & Telemedicine")
                .optimisticDurationRatio(0.88)
                .pessimisticDurationRatio(1.60)
                .averageHistoricalDelayRiskPct(45.0)
                .complianceOverheadMultiplier(1.30)
                .scopeCreepProbability(0.38)
                .primaryRiskFactors(List.of("HIPAA encryption compliance verification", "EHR/EMR FHIR interface protocol version mismatch", "Clinical staff UAT availability bottlenecks"))
                .standardSkillKeywords(List.of("HIPAA", "FHIR", "HL7", "WebRTC", "KMS", "Clinical Portal", "Audit Logging"))
                .phaseDistributionPct(Map.of("Compliance_Arch", 0.20, "Integration", 0.35, "Clinical_Trial", 0.25, "Rollout", 0.20))
                .build());

        // 4. E-Commerce & Retail
        profiles.put("ECOMMERCE", DomainBenchmarkProfile.builder()
                .domainName("E-Commerce & Digital Storefront")
                .optimisticDurationRatio(0.82)
                .pessimisticDurationRatio(1.40)
                .averageHistoricalDelayRiskPct(32.0)
                .complianceOverheadMultiplier(1.10)
                .scopeCreepProbability(0.45)
                .primaryRiskFactors(List.of("High-concurrency checkout race conditions on stock", "Third-party payment gateway outages", "Elasticsearch faceted search re-indexing overhead"))
                .standardSkillKeywords(List.of("React", "Stripe", "Redis", "Elasticsearch", "PostgreSQL", "TailwindCSS", "k6 Load Testing"))
                .phaseDistributionPct(Map.of("Catalog_UI", 0.25, "Cart_Checkout", 0.35, "Merchant_Portal", 0.20, "Stress_Testing", 0.20))
                .build());

        // 5. AI / ML & Deep Learning
        profiles.put("AI_ML", DomainBenchmarkProfile.builder()
                .domainName("AI / Machine Learning & RAG Engine")
                .optimisticDurationRatio(0.75)
                .pessimisticDurationRatio(1.75)
                .averageHistoricalDelayRiskPct(52.0)
                .complianceOverheadMultiplier(1.20)
                .scopeCreepProbability(0.55)
                .primaryRiskFactors(List.of("Data quality gaps and label inconsistency", "Hyperparameter convergence stalling and GPU cost overruns", "Hallucination rates and latency SLA misses on inference"))
                .standardSkillKeywords(List.of("PyTorch", "HuggingFace", "Vector DB", "Qdrant", "LangChain", "FastAPI", "MLflow", "MLOps"))
                .phaseDistributionPct(Map.of("Data_Curation", 0.25, "Model_Training", 0.30, "Inference_RAG", 0.25, "Drift_Telemetry", 0.20))
                .build());

        // 6. Cloud Migration & SRE
        profiles.put("CLOUD_DEVOPS", DomainBenchmarkProfile.builder()
                .domainName("Cloud Infrastructure & SRE")
                .optimisticDurationRatio(0.85)
                .pessimisticDurationRatio(1.55)
                .averageHistoricalDelayRiskPct(42.0)
                .complianceOverheadMultiplier(1.18)
                .scopeCreepProbability(0.35)
                .primaryRiskFactors(List.of("Legacy database replication lag & schema lockups", "IAM policy permission boundary misconfigurations", "Stateful volume failover data drift in Kubernetes"))
                .standardSkillKeywords(List.of("Terraform", "Kubernetes", "AWS EKS", "AWS DMS", "ArgoCD", "Vault", "Prometheus"))
                .phaseDistributionPct(Map.of("IaC_VPC", 0.20, "K8s_Cluster", 0.30, "DB_Replication", 0.30, "Cutover", 0.20))
                .build());

        // 7. Gaming & 3D Interactive
        profiles.put("GAMING", DomainBenchmarkProfile.builder()
                .domainName("Interactive Gaming Experience")
                .optimisticDurationRatio(0.78)
                .pessimisticDurationRatio(1.70)
                .averageHistoricalDelayRiskPct(50.0)
                .complianceOverheadMultiplier(1.05)
                .scopeCreepProbability(0.60)
                .primaryRiskFactors(List.of("Shader performance bottlenecks on low-end GPUs", "Physics engine collision edge cases", "Multiplayer netcode desynchronization"))
                .standardSkillKeywords(List.of("Unity", "Unreal Engine", "C#", "C++", "Shaders", "FMOD", "Blender", "Game AI"))
                .phaseDistributionPct(Map.of("Prototype", 0.20, "Gameplay_Assets", 0.40, "Optimization", 0.25, "Beta_Launch", 0.15))
                .build());

        // 8. Data Engineering & ETL
        profiles.put("DATA_ENGINEERING", DomainBenchmarkProfile.builder()
                .domainName("Enterprise Data Lake & ETL")
                .optimisticDurationRatio(0.84)
                .pessimisticDurationRatio(1.48)
                .averageHistoricalDelayRiskPct(36.0)
                .complianceOverheadMultiplier(1.15)
                .scopeCreepProbability(0.40)
                .primaryRiskFactors(List.of("Upstream source schema changes breaking batch pipelines", "Kafka partition rebalancing consumer lag", "Snowflake compute warehouse query concurrency limits"))
                .standardSkillKeywords(List.of("Kafka", "dbt", "Airflow", "Snowflake", "Python", "SQL", "Great Expectations"))
                .phaseDistributionPct(Map.of("Lakehouse_IaC", 0.20, "Pipelines_dbt", 0.40, "Orchestration", 0.20, "Governance", 0.20))
                .build());

        // 9. Standard Enterprise SaaS Web
        profiles.put("SAAS_WEB", DomainBenchmarkProfile.builder()
                .domainName("Enterprise SaaS Web Platform")
                .optimisticDurationRatio(0.85)
                .pessimisticDurationRatio(1.45)
                .averageHistoricalDelayRiskPct(30.0)
                .complianceOverheadMultiplier(1.10)
                .scopeCreepProbability(0.35)
                .primaryRiskFactors(List.of("Complex multi-tenant permission boundaries", "Third-party integration rate limits", "E2E UI test flakiness"))
                .standardSkillKeywords(List.of("React", "TypeScript", "Java", "Spring Boot", "PostgreSQL", "Docker", "REST API", "TailwindCSS"))
                .phaseDistributionPct(Map.of("Architecture_Design", 0.20, "Core_Services", 0.35, "Frontend_UI", 0.25, "QA_Deploy", 0.20))
                .build());
    }
}
