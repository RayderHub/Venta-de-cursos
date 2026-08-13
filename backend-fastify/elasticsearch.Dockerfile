FROM docker.elastic.co/elasticsearch/elasticsearch:8.15.3

USER root

RUN printf '%s\n' \
    '#!/bin/bash' \
    'set -e' \
    'if [ -n "$PORT" ]; then' \
    '  echo "http.port: ${PORT}" >> /usr/share/elasticsearch/config/elasticsearch.yml' \
    'fi' \
    'exec /bin/tini -- /usr/local/bin/docker-entrypoint.sh eswrapper' \
    > /usr/local/bin/render-entrypoint.sh \
 && chmod +x /usr/local/bin/render-entrypoint.sh \
 && chmod g+ws /usr/share/elasticsearch/config /usr/share/elasticsearch/config/elasticsearch.yml

USER 1000:0

ENTRYPOINT ["/usr/local/bin/render-entrypoint.sh"]
