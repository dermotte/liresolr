import re
import numpy as np
import matplotlib.pyplot as plt
import json
from pathlib import Path
import pandas as pd

home_directory = str(Path.home())

input_file = home_directory + '/projects/wipo2018/weights1299.txt'
state = 0
count_images = 0
count_classes = 0
current_image = ''
data = {}
# weights = []
classes = []

if __name__ == '__main__':
    # parse data into an internal representation (dictionary) for analysis ..

    with open(input_file) as f:
        for line in f:
            l = line.strip()
            if len(l) == 0 :
                if state == 2 and count_classes != 1299:
                    print(current_image + ': ' + str(count_classes))
                state = 0
                count_classes = 0
                if count_images % 100 == 0:  # just to have some status updates ...
                    print("Read data for {} files".format(count_images))
                # if count_images > 4999: break  # for DEBUG: break after a few files ...
            if (l.lower().endswith('.png') or l.lower().endswith('.jpg')) and state == 0:
                # DEBUG: Here the actual file URI should come in.
                current_image = '/home/mlux/projects/wipo2018' + l[2:].replace('\\', '/').replace('//', '/')
                data[current_image] = {}
                count_images += 1
                state = 1
            if state == 1 and l.lower() == 'found classes:' :
                state = 2
            if state == 2 and re.search("\d\d\.\d\d\.\d\d", l) is not None:
                count_classes += 1
                class_id = re.search("\d\d\.\d\d\.\d\d", l).group(0)
                if class_id not in classes: classes.append(class_id)
                weight = float(re.search("\d+\.\d\d$", l).group(0))
                data[current_image][class_id] = weight
                # weights.append(weight)


    print("Finished reading data for {} files".format(count_images))
    # arr = np.array(weights, dtype=float)  # min=0, max=33.62

    # convert it to a pandas data frame by first sorting the classes and then adding them row by row.
    classes = sorted(classes)
    # df = pd.DataFrame(columns=classes)
    count_images = 0
    index_list = data.keys()
    all_data = []
    for d in index_list :
        row = []
        index = 0
        for k in sorted(data[d]):
            if not k == classes[index]:
                print("Index out of order for image {}".format(d))
            row.append(data[d][k])
            index += 1
        if len(row) == len(classes) :
            all_data.append(row)
            #ef = pd.DataFrame(np.array(row).reshape((1,len(classes))), [d], classes)
            #df = pd.concat([df, ef], copy=False)
        else:
            print("Length mismatch, could not add image {}".format(d))
        if count_images % 100 == 0:  # just to have some status updates ...
            print("Merged data frames for {} files".format(count_images))
        count_images += 1
    print('DataFrame created, now saving to disk ...')
    # now save the data frame for future exploitation:
    df = pd.DataFrame(np.array(all_data).reshape(len(index_list), len(classes)), index=index_list, columns=classes)
    df.to_csv(home_directory + "/tmp/weights1299.csv")
    # df.to_csv(home_directory + "/tmp/weights1299.csv.gz", compression='gzip')
    # df.to_json(home_directory + "/tmp/weights1299.json.gz", compression='gzip')
    # df.to_hdf(home_directory + "/tmp/test.h5", 'table')