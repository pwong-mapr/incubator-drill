select name, gpa, (age%cast(gpa as int)) from student where gpa > 3.50;
