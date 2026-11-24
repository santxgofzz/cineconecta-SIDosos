package co.edu.icesi.cineconecta.persistence.mongo.document;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Elemento de Mi lista. Un documento por pareja (perfil, contenido); el índice
 * único {@code uq_my_list_profile_content} impide duplicados incluso ante
 * solicitudes concurrentes. {@code profileId} es referencia lógica a Oracle.
 */
@Document(collection = MyListItemDocument.COLLECTION)
public class MyListItemDocument {
    public static final String COLLECTION = "my_list";

    @Id
    private ObjectId id;
    private Long profileId;
    private String contentId;
    private Instant addedAt;

    protected MyListItemDocument() {}

    public MyListItemDocument(Long profileId, String contentId, Instant addedAt) {
        this.profileId = profileId;
        this.contentId = contentId;
        this.addedAt = addedAt;
    }

    public ObjectId getId() { return id; }
    public Long getProfileId() { return profileId; }
    public String getContentId() { return contentId; }
    public Instant getAddedAt() { return addedAt; }
}
