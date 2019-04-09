storage "consul" {
  address = "CONSUL_STORAGE_ADDRESS"
  path    = "CONSUL_STORAGE_PATH"
  service = "CONSUL_STORAGE_SERVICE_NAME"
  disable_registration = "true"
}

listener "tcp" {
  address = "0.0.0.0:8200"
  tls_cert_file = "/opt/tvault/certs/tvault.crt"
  tls_key_file  = "/opt/tvault/certs/tvault.key"
}

