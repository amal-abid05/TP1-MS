package metier;

import dao.IDao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MetierIMP implements IMetier {

    @Autowired
    private IDao dao;

    @Override
    public double calcul() {
        double val = dao.getValue();
        return val * 7;
    }

    public void setDao(IDao dao) {

        this.dao = dao;
    }
}