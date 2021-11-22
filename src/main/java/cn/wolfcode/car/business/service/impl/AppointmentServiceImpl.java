package cn.wolfcode.car.business.service.impl;

import cn.wolfcode.car.business.domain.Appointment;
import cn.wolfcode.car.business.domain.Statement;
import cn.wolfcode.car.business.mapper.AppointmentMapper;
import cn.wolfcode.car.business.query.AppointmentQuery;
import cn.wolfcode.car.business.service.IAppointmentService;
import cn.wolfcode.car.business.service.IStatementService;
import cn.wolfcode.car.common.base.page.TablePageInfo;
import cn.wolfcode.car.common.exception.BusinessException;
import cn.wolfcode.car.common.util.Convert;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@Transactional
public class AppointmentServiceImpl implements IAppointmentService {

    @Autowired
    private AppointmentMapper appointmentMapper;
    @Autowired
    private IStatementService statementService;


    @Override
    public TablePageInfo<Appointment> query(AppointmentQuery qo) {
        PageHelper.startPage(qo.getPageNum(), qo.getPageSize());
        return new TablePageInfo<Appointment>(appointmentMapper.selectForList(qo));
    }

    @Override
    public void save(Appointment appointment) {
        appointment.setCreateTime(new Date());
        appointmentMapper.insert(appointment);
    }

    @Override
    public Appointment get(Long id) {
        return appointmentMapper.selectByPrimaryKey(id);
    }


    @Override
    public void update(Appointment appointment) {
        //1.合理性的校验
        //只有处于预约中的状态才可以进行编辑操作
        Appointment oldObj = this.get(appointment.getId());
        if (!Appointment.STATUS_APPOINTMENT.equals(oldObj.getStatus())){
            //如果状态不是预约中 不能进行编辑
            throw new BusinessException("只有状态为预约中的预约单才能进行编辑操作");
        }
        appointmentMapper.updateByPrimaryKey(appointment);
    }

    @Override
    public void deleteBatch(String ids) {
        Long[] dictIds = Convert.toLongArray(ids);
        for (Long dictId : dictIds) {
            //合理性校验
            Appointment oldObj = this.get(dictId);
            if (!Appointment.STATUS_APPOINTMENT.equals(oldObj.getStatus())){
                throw new BusinessException("只有处于预约中的预约单才能进行删除");
            }
            appointmentMapper.deleteByPrimaryKey(dictId);
        }
    }

    @Override
    public List<Appointment> list() {
        return appointmentMapper.selectAll();
    }

    @Override
    public void cancel(Long id) {
        Appointment oldObj = this.get(id);
        if (!Appointment.STATUS_APPOINTMENT.equals(oldObj.getStatus())){
            //如果状态不是预约中 不能进行编辑
            throw new BusinessException("只有状态为预约中的预约单才能进行取消");
        }
        appointmentMapper.changeStatus(id,Appointment.STATUS_CANCEL);
    }

    //到店
    @Override
    public void arrival(Long id) {
        Appointment oldObj = this.get(id);
        if (!Appointment.STATUS_APPOINTMENT.equals(oldObj.getStatus())){
            //如果状态不是预约中 不能进行编辑
            throw new BusinessException("只有状态为预约中的预约单才能到店");
        }
        appointmentMapper.arrival(id,Appointment.STATUS_ARRIVAL,new Date());
    }

    @Override
    public Statement generateStatement(Long appointmentId) {
        //获取预约单对象
        Appointment oldObj = this.get(appointmentId);
        //1.合理性判断
        //已到店和已结算 可以点击结算按钮
        if (!(Appointment.STATUS_ARRIVAL.equals(oldObj.getStatus())
                || Appointment.STATUS_SETTLE.equals(oldObj.getStatus()))){
            throw new BusinessException("只有处于已到店和已结算才能进行结算处理");
        }
        //根据预约单ID查询对应结算单对象
        Statement statement = statementService.getByAppointmentId(appointmentId);
        if(statement == null){
            //没有找到记录 说明此预约单没有生成结算单记录
            statement = new Statement();
            statement.setAppointmentId(appointmentId);//关联预约单id
            statement.setCarSeries(oldObj.getCarSeries());//汽车分类
            statement.setActualArrivalTime(oldObj.getActualArrivalTime());//到店时间
            statement.setCreateTime(new Date());//记录创建时间
            statement.setCustomerName(oldObj.getCustomerName());//客户名称
            statement.setCustomerPhone(oldObj.getCustomerPhone());//客户手机号码
            statement.setInfo(oldObj.getInfo());//备注信息
            statement.setServiceType(oldObj.getServiceType());//服务类型(保养或维修)
            statement.setLicensePlate(oldObj.getLicensePlate());//车牌号码
            //保存结算单
            statementService.save(statement);
        }else {
            //说明有结算单记录
        }
        return statement;
    }

    @Override
    public void updateStatus(Long appointmentId, Integer status) {
        appointmentMapper.changeStatus(appointmentId,status);
    }
}
