package cn.wolfcode.car.business.query;

import cn.wolfcode.car.common.base.query.QueryObject;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Calendar;
import java.util.Date;

@Setter
@Getter
public class EmployeeQuery extends QueryObject {
    private String customerName;
    private Integer age;
    private Integer admin;
    private Integer status;
    private Integer dept_id;


    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date beginTime;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date endTime;

    public Date getEndTime() {
        if(this.beginTime != null){
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(this.endTime);
            calendar.set(Calendar.HOUR_OF_DAY,23);
            calendar.set(Calendar.MINUTE,59);
            calendar.set(Calendar.SECOND, 59);
            return calendar.getTime();
        }
        return null;
    }
}
