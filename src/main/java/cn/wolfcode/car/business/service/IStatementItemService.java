package cn.wolfcode.car.business.service;

import cn.wolfcode.car.business.domain.StatementItem;
import cn.wolfcode.car.business.query.StatementItemQuery;
import cn.wolfcode.car.common.base.page.TablePageInfo;

import java.util.List;

public interface IStatementItemService {
    TablePageInfo<StatementItem> query(StatementItemQuery qo);

    void saveItems(List<StatementItem> items);

    void payStatement(Long statementId);

    void removeByStatementId(Long statementId);
}
