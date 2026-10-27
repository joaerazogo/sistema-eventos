package co.com.ticket.dynamodb.order.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

@DynamoDbBean
@Setter
@NoArgsConstructor
public class OrderEntity {

    @Getter(onMethod_ = {@DynamoDbPartitionKey, @DynamoDbAttribute("orderId")})
    private String orderId;

    @Getter(onMethod_ = {@DynamoDbAttribute("eventId"), @DynamoDbSecondaryPartitionKey(indexNames = {"eventId-index"})})
    private String eventId;

    @Getter(onMethod_ = @DynamoDbAttribute("userId"))
    private String userId;

    @Getter(onMethod_ = @DynamoDbAttribute("quantity"))
    private int quantity;

    @Getter(onMethod_ = {@DynamoDbAttribute("status"), @DynamoDbSecondaryPartitionKey(indexNames = {"status-expiresAt-index"})})
    private String status;

    @Getter(onMethod_ = @DynamoDbAttribute("reservedAt"))
    private String reservedAt;

    @Getter(onMethod_ = {@DynamoDbAttribute("expiresAt"), @DynamoDbSecondarySortKey(indexNames = {"status-expiresAt-index"})})
    private String expiresAt;

}
