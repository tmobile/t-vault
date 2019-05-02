ui = true
storage "file" {
  path = "/opt/tvault/hcorp/data"
}

listener "tcp" {
  address = "10.65.228.52:8200"
  tls_cert_file = "/opt/tvault/certs/tvault.crt"
  tls_key_file  = "/opt/tvault/certs/tvault.key"
}
