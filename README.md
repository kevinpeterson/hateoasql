# HATEOASql

Turn your relational database into a [HATEOAS](https://en.wikipedia.org/wiki/HATEOAS) REST Service. Database tables are exposed as REST Resources, and foreign keys are used to build Resource links. Inspired by the [postREST](https://github.com/begriffs/postgrest) project.

## [DEMO](http://hateoasql.kevinp.me/)
The demo uses the [Sakila Sample Database](https://dev.mysql.com/doc/sakila/en/) to demonstrate the linked resources.

__NOTE:__ Best viewed with a JSON rendering browser plugin such as [JSONView for Firefox](https://addons.mozilla.org/en-us/firefox/addon/jsonview/) or [for Chrome](https://chrome.google.com/webstore/detail/chklaanhfefbnpoihckbnefhakgolnmc).

## Installation Prerequisites
* Java 1.7+
* Leiningen 2.0.0+

## Usage
```git clone https://github.com/kevinpeterson/hateoasql.git```

```cd hateoasql```

The following commands will start the service:

    [params] lein ring server [port]    
    
where ```[params]``` can be:
    
    DB_HOST ; Database Host | default 'localhost'
    DB_DRIVER ; Database Driver Class Name | default 'com.mysql.jdbc.Driver'
    DB_PORT ; Database Port | default '3306'
    DB_TYPE ; Database Type | default 'mysql'
    DB_USER ; Database User Name | required
    DB_PASSWORD ; Database Password | default ""
    DB ; Database Name | required
    BASE_URL ; Base URL for HREFs | default 'http://localhost:3000/'

Example:

    DB_USER=myUser DB=myDb BASE_URL=http://someServer.com:5000/ lein ring server 5000    
    
### Resources
HATEOASql assumes that every table in the given database is equivalent to a REST Resource, assuming that the table has a single primary key. Resources will appear as the pluralized version of the table name (thanks to [inflections-clj](https://github.com/r0man/inflections-clj])).

Any HREF ending in an identifier (e.g ```/films```) will return a JSON Array type with all possible Resources for that type. Any HREF ending in an identifier (e.g. ```/films/1```) will return a single JSON Object.

### Resource Links
Resources may have associations to other Resources. This is expressed in as hierarchical HREFs such as a ```/films/1/actors``` that are expressed in the ```links``` section of the JSON Resource.

HATEOASql builds RESTful HREF links by inspecting database foreign keys between tables. If no foreign keys are are asserted, no Resource Links will be built.

## Future
* Enable write via PUT/POST.
* Allow for query parameters for searching.
* Add sorting.
    
## License
MIT
