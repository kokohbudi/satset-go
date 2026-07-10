#!/bin/sh
set -e
# Proxy TERBUKA ke internet — WAJIB auth biar gak jadi open relay.
: "${PROXY_USER:?PROXY_USER wajib di-set (fly secrets set)}"
: "${PROXY_PASS:?PROXY_PASS wajib di-set (fly secrets set)}"
echo "BasicAuth ${PROXY_USER} ${PROXY_PASS}" >> /etc/tinyproxy/tinyproxy.conf
exec tinyproxy -d -c /etc/tinyproxy/tinyproxy.conf
