package com.example.Train_Service.ScheduledMethodsPackage;


import com.example.Train_Service.Service.ServiceClass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Data
@ToString
@Component
public class ScheduledEndpoints {

    private final static Logger log = LoggerFactory.getLogger(ScheduledEndpoints.class);

    private ServiceClass serviceClass;

    public ScheduledEndpoints(ServiceClass serviceClass) {
        this.serviceClass = serviceClass;
    }

    public void sendTatkalAndPremiumTatkalTickets() {
        serviceClass.sendTatkalAndPremiumTataklTickets();
    }

}
