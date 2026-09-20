#!/bin/bash

# 1. Update system packages and install Docker Engine
dnf update -y
dnf install -y docker

# 2. Allocate and enable a 2GB Swap memory file to prevent Out-Of-Memory (OOM) crashes
dd if=/dev/zero of=/swapfile bs=128M count=16
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo "/swapfile swap swap defaults 0 0" >> /etc/fstab

# 3. Create required host directories for ECS state, logs, and Grafana Alloy config
mkdir -p /var/log/ecs /var/lib/ecs/data /etc/ecs /etc/alloy

# 4. Configure ECS Cluster connection and Automated Disk Cleanup rules (24h cycle)
# IMPORTANT: These rules prevent the EC2 disk from filling up with old Docker images over time.
cat <<EOF > /etc/ecs/ecs.config
ECS_CLUSTER=clickfila-cluster
ECS_DATADIR=/var/lib/ecs/data
ECS_IMAGE_CLEANUP_INTERVAL=24h
ECS_IMAGE_MINIMUM_CLEANUP_AGE=3h
ECS_NUM_IMAGES_DELETE_PER_CYCLE=5
ECS_ENGINE_TASK_CLEANUP_WAIT_DURATION=1h
EOF

# 5. Generate Grafana Alloy configuration file for observability (Metrics & Logs)
# It scrapes host metrics, container metrics, Spring Boot actuator, and forwards them to Grafana Cloud.
cat << 'EOF' > /etc/alloy/config.alloy
prometheus.exporter.unix "host_metrics" {
  procfs_path = "/host/proc"
  sysfs_path  = "/host/sys"
  rootfs_path = "/rootfs"
}

prometheus.scrape "unix_scraper" {
  targets         = prometheus.exporter.unix.host_metrics.targets
  forward_to      = [prometheus.remote_write.grafana_cloud.receiver]
  scrape_interval = "15s"
  job_name        = "node"
}

prometheus.exporter.cadvisor "docker_metrics" {
  docker_host = "unix:///var/run/docker.sock"
}

prometheus.scrape "cadvisor_scraper" {
  targets         = prometheus.exporter.cadvisor.docker_metrics.targets
  forward_to      = [prometheus.remote_write.grafana_cloud.receiver]
  scrape_interval = "15s"
  job_name        = "cadvisor"
}

prometheus.scrape "spring_boot" {
  targets = [{
    __address__ = "api:8080",
  }]
  metrics_path    = "/api/actuator/prometheus"
  forward_to      = [prometheus.remote_write.grafana_cloud.receiver]
  scrape_interval = "15s"
  job_name        = "spring-boot"
}

prometheus.remote_write "grafana_cloud" {
  endpoint {
    url = sys.env("GRAFANA_URL")

    basic_auth {
      username = sys.env("GRAFANA_USERNAME")
      password = sys.env("GRAFANA_TOKEN")
    }
  }
}

discovery.docker "containers" {
  host = "unix:///var/run/docker.sock"
}

loki.source.docker "docker_logs" {
  host       = "unix:///var/run/docker.sock"
  targets    = discovery.docker.containers.targets
  forward_to = [loki.write.grafana_cloud.receiver]
}

loki.write "grafana_cloud" {
  endpoint {
    url = sys.env("LOKI_URL")

    basic_auth {
      username = sys.env("LOKI_USERNAME")
      password = sys.env("GRAFANA_TOKEN")
    }
  }
}
EOF

# 6. Enable Docker to start on system boot and start the daemon immediately
systemctl enable docker
systemctl start docker

# 7. Run the Amazon ECS Agent container
# CRITICAL FIX: The --env-file flag and volume mappings force the agent to read the ecs.config rules correctly, ensuring the automated cleanup actually runs.
docker run -d --name ecs-agent \
    --net=host \
    --restart=always \
    --volume=/var/run:/var/run \
    --volume=/var/log/ecs:/log \
    --volume=/var/lib/ecs/data:/var/lib/ecs/data \
    --volume=/etc/ecs:/etc/ecs \
    --env-file=/etc/ecs/ecs.config \
    --env=ECS_LOGFILE=/log/ecs-agent.log \
    amazon/amazon-ecs-agent:latest