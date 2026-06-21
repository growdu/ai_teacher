#!/bin/bash
# ================================================================
# Let's Encrypt SSL Certificate Management for AI Teacher Studio
# ================================================================
# Usage:
#   ./letsencrypt.sh obtain <domain>          - Obtain a new certificate
#   ./letsencrypt.sh renew                     - Renew all certificates
#   ./letsencrypt.sh revoke <domain>            - Revoke a certificate
# ================================================================

set -euo pipefail

SSL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../deploy/nginx/ssl" && pwd)"
WEB_ROOT="/var/www/html"
EMAIL="${LETSENCRYPT_EMAIL:-admin@$(hostname -f)}"

# Ensure SSL directory exists
mkdir -p "$SSL_DIR" "$WEB_ROOT/.well-known/acme-challenge"
chmod -R 755 "$WEB_ROOT"

command -v certbot >/dev/null 2>&1 || {
    echo "Error: certbot not found. Install with: sudo apt install certbot python3-certbot-nginx"
    exit 1
}

obtain() {
    local domain="$1"
    [[ -z "$domain" ]] && echo "Error: domain required" && exit 1

    echo "==> Obtaining Let's Encrypt certificate for: $domain"

    certbot certonly \
        --nginx \
        --non-interactive \
        --agree-tos \
        --email "$EMAIL" \
        --domains "$domain" \
        --expand \
        --key-type ecdsa \
        --elliptic-curve secp384r1 \
        --deploy-hook "systemctl reload nginx" \
        || certbot certonly \
            --webroot \
            --webroot-path "$WEB_ROOT" \
            --non-interactive \
            --agree-tos \
            --email "$EMAIL" \
            --domains "$domain" \
            --expand \
            --key-type ecdsa \
            --elliptic-curve secp384r1 \
            --deploy-hook "systemctl reload nginx"

    # Symlink to expected location
    local cert_dir="/etc/letsencrypt/live/$domain"
    if [[ -d "$cert_dir" ]]; then
        ln -sf "$cert_dir/fullchain.pem" "$SSL_DIR/fullchain.pem"
        ln -sf "$cert_dir/privkey.pem" "$SSL_DIR/privkey.pem"
        echo "==> Certificates installed to $SSL_DIR"
    fi
}

renew() {
    echo "==> Renewing Let's Encrypt certificates..."
    certbot renew \
        --non-interactive \
        --deploy-hook "systemctl reload nginx"
}

revoke() {
    local domain="$1"
    [[ -z "$domain" ]] && echo "Error: domain required" && exit 1

    local cert_path="/etc/letsencrypt/live/$domain/fullchain.pem"
    if [[ -f "$cert_path" ]]; then
        echo "==> Revoking certificate for: $domain"
        sudo certbot revoke --cert-path "$cert_path" --non-interactive
        sudo certbot delete --cert-name "$domain" --non-interactive
        rm -f "$SSL_DIR/fullchain.pem" "$SSL_DIR/privkey.pem"
        echo "==> Certificate revoked and deleted"
    else
        echo "Error: Certificate for $domain not found"
        exit 1
    fi
}

case "${1:-}" in
    obtain)  obtain "$2" ;;
    renew)   renew ;;
    revoke)  revoke "$2" ;;
    *)       echo "Usage: $0 {obtain <domain>|renew|revoke <domain>}" ;;
esac
