## Local Setup

1. Copy `k8s/secrets.yaml.template` to `k8s/secrets.yaml` and fill in values
2. Copy `src/main/resources/application-local.yml.template` to `application-local.yml` with your local credentials
3. Run `docker-compose up -d`
4. Run the Spring Boot app with profile: `--spring.profiles.active=local`