package lecture.fastcampus.ecommerce.fpay.application.service;

import jakarta.annotation.PostConstruct;
import lecture.fastcampus.ecommerce.fpay.application.port.in.GetPaymentInfoUseCase;
import lecture.fastcampus.ecommerce.fpay.application.port.in.PaymentFullfillUseCase;
import lecture.fastcampus.ecommerce.fpay.application.port.out.api.PaymentAPIs;
import lecture.fastcampus.ecommerce.fpay.application.port.out.repository.OrderRepository;
import lecture.fastcampus.ecommerce.fpay.application.port.out.repository.PaymentLedgerRepository;
import lecture.fastcampus.ecommerce.fpay.application.port.out.repository.TransactionTypeRepository;
import lecture.fastcampus.ecommerce.fpay.domain.order.Order;
import lecture.fastcampus.ecommerce.fpay.domain.order.OrderStatus;
import lecture.fastcampus.ecommerce.fpay.domain.payment.PaymentLedger;
import lecture.fastcampus.ecommerce.fpay.domain.payment.PaymentMethod;
import lecture.fastcampus.ecommerce.fpay.domain.payment.TransactionType;
import lecture.fastcampus.ecommerce.fpay.infrastructure.out.pg.toss.response.ResponsePaymentApproved;
import lecture.fastcampus.ecommerce.fpay.representation.request.payment.PaymentApproved;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService implements PaymentFullfillUseCase, GetPaymentInfoUseCase {
    private final PaymentAPIs tossPayment;
    private final OrderRepository orderRepository;
    private final PaymentLedgerRepository paymentLedgerRepository;
    private final Set<TransactionTypeRepository> transactionTypeRepositorySet;

    private final Map<String, TransactionTypeRepository> transactionTypeRepositories = new HashMap<>();
    private TransactionTypeRepository transactionTypeRepository;

    @PostConstruct
    public void init() {
        for (TransactionTypeRepository transactionTypeRepository : transactionTypeRepositorySet) {
            String paymentMethodType = transactionTypeRepository.getClass().getSimpleName().split("TransactionTypeRepository")[0].toLowerCase();
            transactionTypeRepositories.put(paymentMethodType, transactionTypeRepository);
        }
    }

    @Transactional
    @Override
    public String paymentApproved(PaymentApproved paymentInfo) throws IOException {
        verifyOrderIsCompleted(UUID.fromString(paymentInfo.getOrderId()));
        ResponsePaymentApproved response = tossPayment.requestPaymentApprove(paymentInfo);

        if (tossPayment.isPaymentApproved(response.getStatus())) {
            Order completedOrder = orderRepository.findById(UUID.fromString(response.getOrderId()));
            completedOrder.orderPaymentFullFill(response.getPaymentKey());
            paymentLedgerRepository.save(response.toPaymentTransactionEntity());
            PaymentMethod method = PaymentMethod.fromMethodName(response.getMethod());
            initTransactionTypeRepository(method);
            transactionTypeRepository.save(TransactionType.convertToTransactionType(response));

            return "success";
        }

        return "fail";
    }

    @Override
    public List<PaymentLedger> getPaymentInfo(String paymentKey) {
        return paymentLedgerRepository.findAllByPaymentKey(paymentKey);
    }

    @Override
    public PaymentLedger getLatestPaymentInfoOnlyOne(String paymentKey) {
        return paymentLedgerRepository.findOneByPaymentKeyDesc(paymentKey);
    }


    public void verifyOrderIsCompleted(UUID orderId) throws IllegalArgumentException {
        OrderStatus status = orderRepository.findById(orderId).getStatus();
        if (!status.equals(OrderStatus.ORDER_COMPLETED))
            throw new IllegalArgumentException("Order is not completed || Order is already paymented");
    }

    private void initTransactionTypeRepository(PaymentMethod paymentMethod) {
        switch (paymentMethod.toString().toLowerCase()) {
            case "card" -> {
                transactionTypeRepository = transactionTypeRepositories.get("card");
            }
            default -> throw new RuntimeException("Unsupported payment method: " + paymentMethod);
        }
    }
}

