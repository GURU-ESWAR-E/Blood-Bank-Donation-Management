// PLACEHOLDER - Member 1 owns this file, will be replaced on merge
package bloodbank.model;

/**
 * Abstract base class representing a person in the BloodBank system.
 */
public abstract class Person {
    private int id;
    private String name;
    private int age;
    private String bloodGroup;
    private String phone;

    public Person(int id, String name, int age, String bloodGroup, String phone) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.bloodGroup = bloodGroup;
        this.phone = phone;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public abstract String getRole();

    public abstract void displayDetails();
}
