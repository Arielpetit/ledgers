/*
 * Copyright (c) 2018-2024 adorsys GmbH and Co. KG
 * All rights are reserved.
 */

package de.adorsys.ledgers.postings.impl.converter;

import de.adorsys.ledgers.postings.api.domain.LedgerAccountBO;
import de.adorsys.ledgers.postings.db.domain.LedgerAccount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(uses = LedgerMapper.class)
public interface LedgerAccountMapper {

    LedgerAccountBO toLedgerAccountBO(LedgerAccount ledgerAccount);

    @Mappings(value = {
            @Mapping(source = "name", target = "name"),
            @Mapping(source = "ledger", target = "ledger"),
            @Mapping(source = "parent", target = "parent"),
            @Mapping(source = "coa", target = "coa"),
            @Mapping(source = "balanceSide", target = "balanceSide"),
            @Mapping(source = "category", target = "category")
    })
    LedgerAccount toLedgerAccount(LedgerAccountBO ledgerAccount);

    List<LedgerAccountBO> toLedgerAccountsBO(List<LedgerAccount> accounts);
}
