FROM clojure

WORKDIR "/work"

RUN git clone https://github.com/kevinpeterson/hateoasql.git /work

CMD DB_HOST=mysql DB=sakila DB_USER=root DB_PASSWORD=root lein ring server-headless 3000