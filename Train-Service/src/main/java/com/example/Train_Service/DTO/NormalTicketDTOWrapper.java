package com.example.Train_Service.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.Queue;

@Data
@NoArgsConstructor
@ToString
public class NormalTicketDTOWrapper {

    private Queue<NormalTicketDTO> normalTicketDTOQueue;

    private List<TrainCoachNumberDTO> trainCoachNumberDTOList;

    public NormalTicketDTOWrapper(Queue<NormalTicketDTO> normalTicketDTOQueue) {
        this.normalTicketDTOQueue = normalTicketDTOQueue;
    }
}
