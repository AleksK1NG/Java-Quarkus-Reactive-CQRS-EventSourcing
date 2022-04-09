package bankAccount.domain;


import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import java.math.BigDecimal;

@MongoEntity(database = "microservices", collection = "bankAccounts")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BankAccountDocument {

    @BsonProperty(value = "_id")
    private ObjectId objectId;

    @BsonProperty(value = "aggregateId")
    private String aggregateId;

    @BsonProperty(value = "email")
    private String email;

    @BsonProperty(value = "userName")
    private String userName;

    @BsonProperty(value = "address")
    private String address;

    @BsonProperty(value = "balance")
    private BigDecimal balance;
}
