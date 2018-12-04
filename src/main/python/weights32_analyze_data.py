import re
import numpy as np
import matplotlib.pyplot as plt
import json
from pathlib import Path

home_directory = str(Path.home())

input_file = home_directory + '/projects/wipo2018/train/weights32.txt'
state = 0
count_images = 0
count_classes = 0
current_image = ''
data = {}
weights = []

if __name__ == '__main__':
    # parse data into an internal representation (dictionary) for analysis ..
    with open(input_file) as f:
        for line in f:
            l = line.strip()
            if len(l) == 0 :
                if state == 2 and count_classes != 32:
                    print(current_image + ': ' + str(count_classes))
                state = 0
                count_classes = 0
            if (l.lower().endswith('.png') or l.lower().endswith('.jpg')) and state == 0:
                current_image = l[2:]
                data[current_image] = []
                count_images += 1
                state = 1
            if state == 1 and l.lower() == 'found classes:' :
                state = 2
            if state == 2 and re.search("\d\d\.\d\d\.\d\d", l) is not None:
                count_classes += 1
                class_id = re.search("\d\d\.\d\d\.\d\d", l).group(0)
                weight = float(re.search("\d+\.\d\d$", l).group(0))
                data[current_image].append((class_id, weight))
                weights.append(weight)

    print("{} files".format(count_images))
    arr = np.array(weights, dtype=float)  # min=3.95, max=33.62
    json.dump(data, open(home_directory + "/tmp/test.json", 'w'))