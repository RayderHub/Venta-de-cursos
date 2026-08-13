FROM docker.elastic.co/elasticsearch/elasticsearch:7.17.24

USER root

RUN printf '%s\n' \
    '-Xms96m' \
    '-Xmx96m' \
    '-XX:MaxDirectMemorySize=64m' \
    '-XX:-AlwaysPreTouch' \
    > /usr/share/elasticsearch/config/jvm.options.d/render.options

RUN printf '%s\n' \
    '#!/bin/bash' \
    'set -e' \
    'if [ -n "$PORT" ]; then' \
    '  echo "http.port: ${PORT}" >> /usr/share/elasticsearch/config/elasticsearch.yml' \
    'fi' \
    'exec /bin/tini -- /usr/local/bin/docker-entrypoint.sh eswrapper' \
    > /usr/local/bin/render-entrypoint.sh \
 && chmod +x /usr/local/bin/render-entrypoint.sh \
 && chmod g+ws /usr/share/elasticsearch/config /usr/share/elasticsearch/config/elasticsearch.yml \
 && echo "node.ml: false" >> /usr/share/elasticsearch/config/elasticsearch.yml \
 && echo "xpack.ml.enabled: false" >> /usr/share/elasticsearch/config/elasticsearch.yml

USER 1000:0

ENTRYPOINT ["/usr/local/bin/render-entrypoint.sh"]
