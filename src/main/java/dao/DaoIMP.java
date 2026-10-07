package dao;


import org.springframework.stereotype.Repository;

@Repository
public class DaoIMP implements IDao {

    @Override
    public double getValue() {
        return 3;
    }
}