package net.ugona.plus;

public class CarActivity extends MainActivity {

    Cars.Car[] getCars() {
        Cars.Car[] res = new Cars.Car[1];
        Cars.Car car = new Cars.Car();
        car.id = car_id;
        car.name = preferences.getString(Names.Car.CAR_NAME + car_id, "");
        res[0] = car;
        return res;
    }

    int menuId() {
        return R.menu.car;
    }
}
