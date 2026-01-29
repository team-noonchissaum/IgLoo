package noonchissaum.backend.domain.order.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.order.service.PaymentService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/")
public class PaymentController {

    private final PaymentService paymentService;


}
