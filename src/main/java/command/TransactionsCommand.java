package command;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement(name = "transactions")
@XmlAccessorType(XmlAccessType.FIELD)
public class TransactionsCommand {
    @XmlAttribute(name = "account")
    private String account;

    @XmlElements({
            @XmlElement(name = "order", type = OrderCommand.class),
            @XmlElement(name = "cancel", type = CancelCommand.class),
            @XmlElement(name = "query", type = QueryCommand.class)
    })
    private List<Object> commands;

    // Getters and Setters
}