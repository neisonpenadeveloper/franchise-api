package com.accenture.franchise.integration;

import com.accenture.franchise.domain.exception.ConcurrentUpdateException;
import com.accenture.franchise.domain.model.Franchise;
import com.accenture.franchise.domain.port.out.FranchiseRepositoryPort;
import com.accenture.franchise.infrastructure.adapter.out.mongo.document.FranchiseDocument;
import de.flapdoodle.embed.mongo.commands.ServerAddress;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.embed.process.io.ProcessOutput;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.transitions.Start;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba la aplicacion completa contra una base de datos real: peticion HTTP ->
 * controlador -> caso de uso -> adaptador -> Mongo, sin simular nada.
 *
 * <p>Los demas tests simulan el puerto de persistencia, asi que hay cosas que
 * solo se pueden comprobar aqui: que el mapeo a documento y de vuelta no pierda
 * datos, que las consultas que Spring Data deriva del nombre del metodo hagan
 * lo que su nombre promete, que el indice unico exista de verdad y que el
 * bloqueo optimista funcione contra el motor y no solo contra un mock.</p>
 *
 * <p>El mongod lo arranca la propia build (Mongo embebido) en un puerto libre.
 * Se prefirio a Testcontainers porque no exige que la maquina tenga Docker: la
 * build corre igual en local que en CI.</p>
 */
@SpringBootTest
@AutoConfigureWebTestClient
class FranchiseIntegrationTest {

    private static final String BASE_URL = "/api/v1/franchises";

    /**
     * Se arranca en un campo estatico y no en un {@code @BeforeAll} porque
     * {@link DynamicPropertySource} se evalua mientras se construye el contexto
     * de Spring: cuando Spring pregunta por la URI, el mongod ya tiene que estar
     * escuchando.
     */
    private static final TransitionWalker.ReachedState<RunningMongodProcess> MONGOD =
            Mongod.instance()
                    // Sin esto, mongod vuelca su log completo en la salida de la
                    // build y tapa el resultado de los tests.
                    .withProcessOutput(Start.to(ProcessOutput.class).initializedWith(ProcessOutput.silent()))
                    .start(Version.Main.V7_0);

    @DynamicPropertySource
    static void mongoUri(DynamicPropertyRegistry registry) {
        ServerAddress address = MONGOD.current().getServerAddress();
        registry.add("spring.data.mongodb.uri",
                () -> "mongodb://" + address.getHost() + ":" + address.getPort() + "/franchisedb_test");
    }

    @AfterAll
    static void detenerMongo() {
        MONGOD.close();
    }

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private FranchiseRepositoryPort repository;

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    /**
     * Se borran los documentos, no la coleccion: al soltar la coleccion se
     * perderia el indice unico, que solo se crea al arrancar el contexto.
     */
    @BeforeEach
    void limpiarLaBaseDeDatos() {
        mongoTemplate.remove(new Query(), FranchiseDocument.class).block();
    }

    @Test
    @DisplayName("el criterio 7 devuelve un producto por sucursal despues de ir y volver de Mongo")
    void flujoCompletoHastaElProductoConMasStock() {
        String franchiseId = crearFranquicia("Franquicia Centro");
        String norte = agregarSucursal(franchiseId, "Sucursal Norte");
        String sur = agregarSucursal(franchiseId, "Sucursal Sur");

        agregarProducto(franchiseId, norte, "Cafe 500g", 120);
        agregarProducto(franchiseId, norte, "Te verde", 45);
        agregarProducto(franchiseId, sur, "Pan", 7);
        agregarProducto(franchiseId, sur, "Leche", 90);

        // Un elemento por sucursal, no el maximo global: si devolviera solo el
        // maximo de la franquicia, "Leche" no apareceria.
        webClient.get().uri(BASE_URL + "/" + franchiseId + "/top-stock-products")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].branchName").isEqualTo("Sucursal Norte")
                .jsonPath("$[0].product.name").isEqualTo("Cafe 500g")
                .jsonPath("$[0].product.stock").isEqualTo(120)
                .jsonPath("$[1].branchName").isEqualTo("Sucursal Sur")
                .jsonPath("$[1].product.name").isEqualTo("Leche")
                .jsonPath("$[1].product.stock").isEqualTo(90);
    }

    @Test
    @DisplayName("modificar el stock y eliminar un producto quedan guardados en Mongo")
    void lasMutacionesPersisten() {
        String franchiseId = crearFranquicia("Franquicia Norte");
        String branchId = agregarSucursal(franchiseId, "Sucursal Unica");
        String cafeId = agregarProducto(franchiseId, branchId, "Cafe 500g", 10);
        String teId = agregarProducto(franchiseId, branchId, "Te verde", 5);

        webClient.patch()
                .uri(BASE_URL + "/" + franchiseId + "/branches/" + branchId + "/products/" + cafeId + "/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("stock", 99))
                .exchange()
                .expectStatus().isOk();

        webClient.delete()
                .uri(BASE_URL + "/" + franchiseId + "/branches/" + branchId + "/products/" + teId)
                .exchange()
                .expectStatus().isOk();

        // Se vuelve a leer desde la base de datos, para comprobar que no era
        // solo la respuesta en memoria de la peticion anterior.
        webClient.get().uri(BASE_URL + "/" + franchiseId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.branches[0].products.length()").isEqualTo(1)
                .jsonPath("$.branches[0].products[0].name").isEqualTo("Cafe 500g")
                .jsonPath("$.branches[0].products[0].stock").isEqualTo(99);
    }

    @Test
    @DisplayName("no se admiten dos franquicias con el mismo nombre, aunque cambie el uso de mayusculas")
    void nombreDeFranquiciaDuplicado() {
        crearFranquicia("Franquicia Centro");

        webClient.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "franquicia centro"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.error").isEqualTo("DUPLICATE_NAME");
    }

    @Test
    @DisplayName("renombrar una franquicia a su propio nombre no cuenta como conflicto")
    void renombrarNoChocaConsigoMisma() {
        String franchiseId = crearFranquicia("Franquicia Centro");

        // Ejercita la consulta derivada existsByNameIgnoreCaseAndIdNot: si no
        // excluyera la propia franquicia, esto responderia 409.
        webClient.patch().uri(BASE_URL + "/" + franchiseId + "/name")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Franquicia Centro"))
                .exchange()
                .expectStatus().isOk();

        webClient.patch().uri(BASE_URL + "/" + franchiseId + "/name")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("name", "Franquicia Renovada"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Franquicia Renovada");
    }

    @Test
    @DisplayName("guardar una version caduca falla en vez de pisar el cambio ajeno")
    void bloqueoOptimistaContraMongo() {
        String franchiseId = crearFranquicia("Franquicia Centro");

        Franchise leidaPorLasDos = repository.findById(franchiseId).block();
        assertThat(leidaPorLasDos).isNotNull();
        assertThat(leidaPorLasDos.version()).isNotNull();

        // La primera escritura gana y deja el documento en una version nueva.
        Franchise ganadora = repository.save(leidaPorLasDos.renameTo("Renombrada por la primera")).block();
        assertThat(ganadora).isNotNull();
        assertThat(ganadora.version()).isGreaterThan(leidaPorLasDos.version());

        // La segunda sigue con la version vieja: sin bloqueo optimista
        // sobrescribiria el cambio anterior sin avisar a nadie.
        StepVerifier.create(repository.save(leidaPorLasDos.renameTo("Renombrada por la segunda")))
                .expectError(ConcurrentUpdateException.class)
                .verify();

        webClient.get().uri(BASE_URL + "/" + franchiseId)
                .exchange()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Renombrada por la primera");
    }

    // ---------- Apoyo ----------

    private String crearFranquicia(String name) {
        Map<String, Object> franquicia = post(BASE_URL, Map.of("name", name));
        return (String) franquicia.get("id");
    }

    /** Devuelve el id de la sucursal recien agregada, que es la ultima de la lista. */
    private String agregarSucursal(String franchiseId, String name) {
        Map<String, Object> franquicia = post(BASE_URL + "/" + franchiseId + "/branches", Map.of("name", name));
        return ultimoId(sucursales(franquicia));
    }

    /** Devuelve el id del producto recien agregado, que es el ultimo de su sucursal. */
    private String agregarProducto(String franchiseId, String branchId, String name, int stock) {
        Map<String, Object> franquicia = post(
                BASE_URL + "/" + franchiseId + "/branches/" + branchId + "/products",
                Map.of("name", name, "stock", stock));
        Map<String, Object> sucursal = sucursales(franquicia).stream()
                .filter(branch -> branchId.equals(branch.get("id")))
                .findFirst()
                .orElseThrow();
        return ultimoId(productos(sucursal));
    }

    private Map<String, Object> post(String uri, Map<String, Object> body) {
        Map<String, Object> response = webClient.post().uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                })
                .returnResult()
                .getResponseBody();
        assertThat(response).isNotNull();
        return response;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> sucursales(Map<String, Object> franquicia) {
        return (List<Map<String, Object>>) franquicia.get("branches");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> productos(Map<String, Object> sucursal) {
        return (List<Map<String, Object>>) sucursal.get("products");
    }

    private static String ultimoId(List<Map<String, Object>> elementos) {
        return (String) elementos.get(elementos.size() - 1).get("id");
    }
}
