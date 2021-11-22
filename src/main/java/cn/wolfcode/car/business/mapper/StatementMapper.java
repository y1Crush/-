package cn.wolfcode.car.business.mapper;

import cn.wolfcode.car.business.domain.Statement;
import cn.wolfcode.car.business.query.StatementQuery;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface StatementMapper {
    int deleteByPrimaryKey(Long id);

    int insert(Statement record);

    Statement selectByPrimaryKey(Long id);

    List<Statement> selectAll();

    int updateByPrimaryKey(Statement record);

    List<Statement> selectForList(StatementQuery qo);

    void updateByAmount(@Param("statementId") Long statementId, @Param("totalAmount") BigDecimal totalAmount, @Param("totalQuantity") BigDecimal totalQuantity, @Param("disCountPrice") BigDecimal disCountPrice);

    void updatePayStatus(@Param("statementId") Long statementId, @Param("status") Integer status, @Param("userId") Long userId);

    Statement getByAppointmentId(Long appointmentId);
}