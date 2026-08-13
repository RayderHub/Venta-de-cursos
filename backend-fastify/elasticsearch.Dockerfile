FROM docker.elastic.co/elasticsearch/elasticsearch:8.15.3

RUN chmod g+ws /usr/share/elasticsearch/config

USER 1000:0
