# [ ] Create the Animal Names program, run tests
print("Hi! Can you name 4 animals? \nIf you want to terminate the program early type 'exit'")
num_animal = 0
all_animals = ("")

while num_animal < 4:
    animal_name = input("Name an animal: ").lower()
    if animal_name == 'exit'.lower():
        break
    else:
        all_animals = animal_name + all_animals
        num_animal += 1
        
print("You list of animals is", all_animals)
