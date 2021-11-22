package cn.wolfcode.car.business.service.impl;

import cn.wolfcode.car.business.domain.Statement;
import cn.wolfcode.car.business.mapper.StatementMapper;
import cn.wolfcode.car.business.query.StatementQuery;
import cn.wolfcode.car.business.service.IStatementItemService;
import cn.wolfcode.car.business.service.IStatementService;
import cn.wolfcode.car.common.base.page.TablePageInfo;
import cn.wolfcode.car.common.exception.BusinessException;
import cn.wolfcode.car.common.util.Convert;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class StatementServiceImpl implements IStatementService {

    @Autowired
    private StatementMapper statementMapper;
    @Autowired
    private IStatementItemService statementItemService;

    @Override
    public TablePageInfo<Statement> query(StatementQuery qo) {
        PageHelper.startPage(qo.getPageNum(), qo.getPageSize());
        return new TablePageInfo<Statement>(statementMapper.selectForList(qo));
    }

    @Override
    public void save(Statement statement) {
        //设置状态
        statement.setStatus(Statement.STATUS_CONSUME);

        statement.setCreateTime(new Date());
        statementMapper.insert(statement);
    }

    @Override
    public Statement get(Long id) {
        return statementMapper.selectByPrimaryKey(id);
    }


    @Override
    public void update(Statement statement) {
        //合理性校验
        //1.只有消费中的结算单才能进行编辑
        Statement oldObj = this.get(statement.getId());
        if (Statement.STATUS_PAID.equals(oldObj.getStatus())) {
            throw new BusinessException("只有处于消费中的阶段但才能进行编辑");
        }
        statementMapper.updateByPrimaryKey(statement);
    }

    @Override
    @Transactional
    public void deleteBatch(String ids) {
        Long[] dictIds = Convert.toLongArray(ids);
        for (Long dictId : dictIds) {
            statementMapper.deleteByPrimaryKey(dictId);//dictId就是StatementId
            statementItemService.removeByStatementId(dictId);
        }
    }

    @Override
    public List<Statement> list() {
        return statementMapper.selectAll();
    }

    @Override
    public void updateAmount(Long statementId, BigDecimal totalAmount, BigDecimal totalQuantity, BigDecimal disCountPrice) {
        statementMapper.updateByAmount(statementId, totalAmount, totalQuantity, disCountPrice);
    }

    @Override
    public void updatePayStatus(Long statementId, Integer status, Long userId) {
        statementMapper.updatePayStatus(statementId, status, userId);
    }

    @Override
    public Statement getByAppointmentId(Long appointmentId) {
        return statementMapper.getByAppointmentId(appointmentId);
    }
}
