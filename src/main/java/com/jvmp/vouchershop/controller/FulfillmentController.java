package com.jvmp.vouchershop.controller;

import com.jvmp.vouchershop.fulfillment.Fulfillment;
import com.jvmp.vouchershop.fulfillment.FulfillmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api")
@RequiredArgsConstructor
@RestController
@CrossOrigin
@Slf4j
public class FulfillmentController {

    private final FulfillmentService fulfillmentService;

    @GetMapping("/fulfillments/{order_id}")
    public Fulfillment getFulfillmentForOrder(@PathVariable("order_id") long orderId) {
        return fulfillmentService.findByOrderId(orderId);
    }

}
