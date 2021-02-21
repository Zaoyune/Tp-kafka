package org.sid.demospringcloudstreamskafka.web;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.*;
import org.sid.demospringcloudstreamskafka.Entities.PageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

@CrossOrigin("*")
@RestController
public class PageEventRestController {
    @Autowired
    private StreamBridge streamBridge;
    @Autowired
    private InteractiveQueryService interactiveQueryService;
    @GetMapping("/publish/{topic}/{name}")
    public PageEvent publish(@PathVariable String topic,@PathVariable String name){
        PageEvent pageEvent = new PageEvent(name,Math.random()>0.5?"U1":"U2",new Date(),new Random().nextInt(9000));
        streamBridge.send(topic,pageEvent);
        return pageEvent;
    }
    @CrossOrigin("*")
    @GetMapping(path = "/analytics",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String, Long>> analytics(){//Flux => un stream
        return Flux.interval(Duration.ofSeconds(1))//demander a ce flux de générer un enregistrement chaque 1 seconde
                .map(sequence->{//et chaque seconde créer un objet de type hashmap et envoyer au cliant le resultat récupérer a partir du store par ces étapes
                    Map<String, Long> stringLongMap = new HashMap<>();
                    ReadOnlyWindowStore<String, Long> windowStore = interactiveQueryService.getQueryableStore("page-count", QueryableStoreTypes.windowStore());
                    Instant now=Instant.now();
                    Instant from=now.minusMillis(5000);
                    KeyValueIterator<Windowed<String>, Long> fetchAll = windowStore.fetchAll(from, now);//fetchAll => selectionner et afficher tout les données pendants les 5 dérnières secondes
                    //WindowStoreIterator<Long> fetch = windowStore.fetch("P1",from,now);//affiche les enregistrements ayant la clée P1
                    while (fetchAll.hasNext()){//parcourir les resultat pour récuperer un objet de type keyvalue
                        KeyValue<Windowed<String>,Long> next = fetchAll.next();//next=>l'enregistrement
                        stringLongMap.put(next.key.key(),next.value);//la methode key nous permet de retourner la clée qui est le nom de la page et la valeur est le nombre de visite de la page
                    }
                    return stringLongMap;//après avoir stocker les donée sur stringLongMap je les retourne
                }).share();//permet à ce flux d'etre partager a tout les utilisateurs qui se connectent sinon en peut l'enlever et laisser chaque utilisateur quis e connecte recois son propre flux
    }
    @GetMapping(path = "/analytics/{page}",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String, Long>> analyticss(@PathVariable String page){//Flux => un stream
        return Flux.interval(Duration.ofSeconds(1))//demander a ce flux de générer un enregistrement chaque 1 seconde
                .map(sequence->{//et chaque seconde créer un objet de type hashmap et envoyer au cliant le resultat récupérer a partir du store par ces étapes
                    Map<String, Long> stringLongMap = new HashMap<>();
                    ReadOnlyWindowStore<String, Long> windowStore = interactiveQueryService.getQueryableStore("page-count", QueryableStoreTypes.windowStore());
                    Instant now=Instant.now();
                    Instant from=now.minusMillis(5000);
                    //KeyValueIterator<Windowed<String>, Long> fetchAll = windowStore.fetchAll(from, now);//fetchAll => selectionner et afficher tout les données pendants les 5 dérnières secondes
                    WindowStoreIterator<Long> fetchAll = windowStore.fetch(page,from,now);//affiche les enregistrements ayant la clée P1
                    while (fetchAll.hasNext()){//parcourir les resultat pour récuperer un objet de type keyvalue
                        KeyValue<Long,Long> next = fetchAll.next();//next=>l'enregistrement
                        stringLongMap.put(page,next.value);//la methode key nous permet de retourner la clée qui est le nom de la page et la valeur est le nombre de visite de la page
                    }
                    return stringLongMap;//après avoir stocker les donée sur stringLongMap je les retourne
                }).share();//permet à ce flux d'etre partager a tout les utilisateurs qui se connectent sinon en peut l'enlever et laisser chaque utilisateur quis e connecte recois son propre flux
    }
}
