package com.bank.fedwire.repository;

import com.bank.fedwire.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessingPipelineRepository extends JpaRepository<Transaction, Long> {

    @Query(value = """
            select
                t.transaction_id as transactionId,
                t.transaction_status as transactionStatus,
                t.transaction_date_time as transactionDateTime,
                p8.pacs008_id as pacs008Id,
                p8.xml_payload as pacs008XmlPayload,
                p8.created_date as pacs008CreatedDate,
                p8.sqs_published_at as pacs008SentAt,
                p8.message_id as pacs008MessageId,
                p2.pacs002_id as pacs002Id,
                p2.transaction_status as pacs002TransactionStatus,
                p2.received_timestamp as pacs002ReceivedTimestamp,
                a.adm002_id as admi002Id,
                a.received_timestamp as admi002ReceivedTimestamp,
                st_beneficiary.created_at as beneficiarySettlementAt,
                st_return.created_at as returnSettlementAt
            from transactions t
            left join pacs008 p8
                on p8.transaction_id = t.transaction_id
            left join pacs002 p2
                on p2.pacs002_id = (
                    select p2_latest.pacs002_id
                    from pacs002 p2_latest
                    where p2_latest.transaction_id = t.transaction_id
                    order by p2_latest.received_timestamp desc, p2_latest.pacs002_id desc
                    limit 1
                )
            left join adm002 a
                on a.adm002_id = (
                    select a_latest.adm002_id
                    from adm002 a_latest
                    where a_latest.transaction_id = t.transaction_id
                    order by a_latest.received_timestamp desc, a_latest.adm002_id desc
                    limit 1
                )
            left join settlement_transactions st_beneficiary
                on st_beneficiary.settlement_transaction_id = (
                    select st_latest.settlement_transaction_id
                    from settlement_transactions st_latest
                    where st_latest.payment_id = t.transaction_id
                      and st_latest.transaction_type = 'CREDIT_TO_BENEFICIARY'
                      and st_latest.status = 'SUCCESS'
                    order by st_latest.created_at desc, st_latest.settlement_transaction_id desc
                    limit 1
                )
            left join settlement_transactions st_return
                on st_return.settlement_transaction_id = (
                    select st_latest.settlement_transaction_id
                    from settlement_transactions st_latest
                    where st_latest.payment_id = t.transaction_id
                      and st_latest.transaction_type = 'RETURN_TO_SENDER'
                      and st_latest.status = 'SUCCESS'
                    order by st_latest.created_at desc, st_latest.settlement_transaction_id desc
                    limit 1
                )
            where t.transaction_id = :transactionId
            """, nativeQuery = true)
    Optional<ProcessingPipelineProjection> findPipelineByTransactionId(@Param("transactionId") Long transactionId);
}
