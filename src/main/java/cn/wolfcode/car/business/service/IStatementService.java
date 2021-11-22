package cn.wolfcode.car.business.service;


import cn.wolfcode.car.business.domain.Statement;
import cn.wolfcode.car.business.query.StatementQuery;
import cn.wolfcode.car.common.base.page.TablePageInfo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 养修服务结算单接口
 */
public interface IStatementService {

    /**
     * 分页
     * @param qo
     * @return
     */
    TablePageInfo<Statement> query(StatementQuery qo);


    /**
     * 查单个
     * @param id
     * @return
     */
    Statement get(Long id);


    /**
     * 保存
     * @param statement
     */
    void save(Statement statement);

  
    /**
     * 更新
     * @param statement
     */
    void update(Statement statement);

    /**
     *  批量删除
     * @param ids
     */
    void deleteBatch(String ids);

    /**
     * 查询全部
     * @return
     */
    List<Statement> list();

    void updateAmount(Long statementId, BigDecimal totalAmount, BigDecimal totalQuantity, BigDecimal disCountPrice);

    void updatePayStatus(Long statementId, Integer status, Long userId);

    Statement getByAppointmentId(Long appointmentId);
}
