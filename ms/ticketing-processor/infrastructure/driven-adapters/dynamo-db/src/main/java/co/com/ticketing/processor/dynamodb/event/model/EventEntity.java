package co.com.ticketing.processor.dynamodb.event.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
@Setter
@NoArgsConstructor
public class EventEntity {

    @Getter(onMethod_ = {@DynamoDbPartitionKey, @DynamoDbAttribute("eventId")})
    private String eventId;

    @Getter(onMethod_ = @DynamoDbAttribute("name"))
    private String name;

    @Getter(onMethod_ = @DynamoDbAttribute("venue"))
    private String venue;

    @Getter(onMethod_ = @DynamoDbAttribute("date"))
    private String date;

    @Getter(onMethod_ = @DynamoDbAttribute("totalCapacity"))
    private int totalCapacity;

    @Getter(onMethod_ = @DynamoDbAttribute("availableTickets"))
    private int availableTickets;

}
