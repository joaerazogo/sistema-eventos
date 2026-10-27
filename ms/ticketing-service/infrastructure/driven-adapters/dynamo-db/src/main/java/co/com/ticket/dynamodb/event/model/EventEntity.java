package co.com.ticket.dynamodb.event.model;

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
public class EventEntity {

    @Getter(onMethod_ = {@DynamoDbPartitionKey, @DynamoDbAttribute("eventId")})
    private String eventId;

    @Getter(onMethod_ = @DynamoDbAttribute("name"))
    private String name;

    @Getter(onMethod_ = {@DynamoDbAttribute("venue"), @DynamoDbSecondaryPartitionKey(indexNames = "venue-date-index")})
    private String venue;

    @Getter(onMethod_ = {@DynamoDbAttribute("date"), @DynamoDbSecondarySortKey(indexNames = "venue-date-index")})
    private String date;

    @Getter(onMethod_ = @DynamoDbAttribute("totalCapacity"))
    private int totalCapacity;

    @Getter(onMethod_ = @DynamoDbAttribute("availableTickets"))
    private int availableTickets;

    @Getter(onMethod_ = @DynamoDbAttribute("version"))
    private Long version;

}
