# HATEOASql

Turn your relational database into a [HATEOAS](https://en.wikipedia.org/wiki/HATEOAS) REST Service. 

# [DEMO](https://hateoasql.herokuapp.com/)
The demo uses the [Sakila Sample Database](https://dev.mysql.com/doc/sakila/en/) to demonstrate the linked resources.
__NOTE:__ Best viewed with a JSON rendering browser plugin such as [JSONView for Firefox](https://addons.mozilla.org/en-us/firefox/addon/jsonview/) or [for Chrome](https://chrome.google.com/webstore/detail/chklaanhfefbnpoihckbnefhakgolnmc).

## Usage

    [params] lein ring server [port]    
    
where ```[params]``` can be:
    
    DB_HOST={dbHost} ; default 'localhost'
    DB_DRIVER={dbHost} ; default 'com.mysql.jdbc.Driver'
    DB_PORT={dbPort} ; default '3306'
    DB_TYPE={dbType} ; default 'mysql'
    DB_PASSWORD={dbPassword} ; required
    DB_USER={user} ; default ""
    DB={dbName} ; required

Example:

    DB_USER=myUser DB=myDb lein ring server 5000    
    
## License

MIT
