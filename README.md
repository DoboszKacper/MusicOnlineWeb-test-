# MusicOnlineWeb-test-

## Muisc Online Web(Vertx)
  #### Tutaj znajdują sie aplikacje w vert.x
  CustomerApi oraz MuiscApi zrobiłem na dwa różne sposoby,
  
  ### Moduł CustomerApi:
  Posiada w sobie dwie klasy, jedna łaczy sie z baza danych klientów oraz tworzy server, a droga wyswietla poprzez service discovery te dane na ekranie i jest to uruchamiane poprzez taki kod
  ##  
     Vertx v = Vertx.vertx();
        v.deployVerticle(new CustomerDatabaseConnection(),stringAsyncResult -> {
            v.deployVerticle(new ConnectToCustomer());
        });
        
  ## Działa
        
        
 ### Moduł MusicApi i MusicDisplay: 
  MusicApi ma w sobie jedna klase do łaczenia sie z baza i mozna odplic ja samodzielnie, natomiast moduł MuiscDisplay służy do połaczenia porzez service discovery z MusicApi. 
  ## Nie Działa
  
  
  ## Docker for mow
  #### Tutaj znajduja sie pliki oraz docker compose do odpalenia baz mysql
  "docker-compose up" - (do odpalenia)
