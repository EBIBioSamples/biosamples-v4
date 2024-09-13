import copy
import json
import random
import threading
from os import listdir
from os.path import isfile, join
from queue import Queue

import matplotlib.pyplot as plt
import requests
import sys
import time
from prettytable import PrettyTable
from tqdm import tqdm

# to run: python load_test_bulk_sample_submission.py -user <> --password <> --sample_path <>

WEBIN_AUTH_ENDPOINT = "https://wwwdev.ebi.ac.uk/ena/submit/webin/auth/token"
BULK_SUBMISSION_ENDPOINT = "http://wp-np2-44:8082/biosamples/v2/samples/bulk-submit"

time_per_sample_queue = Queue()
base_samples = []


def submit_samples(samples, token):
    headers = {"accept": "application/json", "authorization": "Bearer " + token}
    response = requests.post(BULK_SUBMISSION_ENDPOINT, headers=headers, json=samples)
    response.raise_for_status()
    data = None
    if response.status_code == 201:
        data = response.json()

    return data


def authenticate(username, password):
    headers = {"accept": "application/json"}
    data = {
        "authRealms": ["ENA"],
        "username": username,
        "password": password
    }
    response = requests.post(WEBIN_AUTH_ENDPOINT, headers=headers, json=data)
    return response.text


def load_samples_from_disk(file_path):
    if len(base_samples) != 0:
        return

    sample_files = [join(file_path, f) for f in listdir(file_path) if isfile(join(file_path, f))]
    for s in sample_files:
        try:
            with open(s, 'r') as file:
                base_samples.append(json.load(file))
        except Exception as e:
            print(f"Error reading file {file_path}: {e}")
            sys.exit(1)


def generate_samples(count):
    samples = []
    base_sample_count = len(base_samples)
    for i in range(count):
        s = copy.deepcopy(base_samples[random.randint(0, base_sample_count - 1)])
        s['name'] = s['name'] + str(i)
        samples.append(s)
    return samples


def measure_bulk_submission_time(no_of_samples):
    load_samples_from_disk(sample_path)
    samples = generate_samples(no_of_samples)
    token = authenticate(user, password)
    start_time = time.time()
    response = submit_samples(samples, token)
    end_time = time.time()
    elapsed_time_ms = (end_time - start_time) * 1000
    # print(f"Submitted {no_of_samples} samples in %.2fms" % elapsed_time_ms)
    # print(f"Time per sample: %.2fms" % (elapsed_time_ms / no_of_samples))

    if len(response['samples']) != no_of_samples:
        print(f"Validation failure. {len(response['errors'])} out of {no_of_samples} samples failed validation")

    time_per_sample = elapsed_time_ms / no_of_samples
    time_per_sample_queue.put(time_per_sample)
    return time_per_sample


def run_multiple_submissions_parallel(no_of_samples, times_to_run):
    test = []
    for i in range(times_to_run):
        t = threading.Thread(target=measure_bulk_submission_time, args=(no_of_samples,))
        test.append(t)
        t.start()

    for t in test:
        t.join()

    total_time_ms = 0
    while not time_per_sample_queue.empty():
        total_time_ms = total_time_ms + time_per_sample_queue.get()

    time_per_sample = total_time_ms / times_to_run
    # print(f"Average time per sample {time_per_sample}")
    return time_per_sample


def run_multiple_submissions_serial(no_of_samples, times_to_run):
    total_time_ms = 0
    for i in range(times_to_run):
        total_time_ms = total_time_ms + measure_bulk_submission_time(no_of_samples)

    time_per_sample = total_time_ms / times_to_run
    # print(f"Average time per sample {time_per_sample}")
    return time_per_sample


def draw_table(header, data):
    table = PrettyTable(header)
    for row in data:
        table.add_row(row)
    print(table)


def draw_graph(headers, rows):
    x = headers[1:]
    for row in rows:
        y = [float(x) for x in row[1:]]
        plt.plot(x, y, label='# samples: ' + str(row[0]))

    plt.xlabel("# runs")
    plt.ylabel("Time (ms)")
    plt.legend()
    plt.title('Average sample submission time')
    plt.show()


def run(no_of_sample_list, times_to_run_list):
    # serial submissions
    headers = ["number of samples"]
    headers.extend([x for x in times_to_run_list])
    rows = []
    for no_of_samples in tqdm(no_of_sample_list):
        row = [no_of_samples]
        for times_to_run in times_to_run_list:
            avg_time = run_multiple_submissions_serial(no_of_samples, times_to_run)
            row.append(f" %.2f" % avg_time)
        rows.append(row)
        time.sleep(sleep_between_iteration)
    draw_table(headers, rows)
    draw_graph(headers, rows)

    # parallel submissions
    # headers = ["number of samples"]
    # headers.extend(["threads = " + str(x) for x in times_to_run_list])
    # rows = []
    # for no_of_samples in no_of_sample_list:
    #     row = [no_of_samples]
    #     for times_to_run in times_to_run_list:
    #         avg_time = run_multiple_submissions_parallel(no_of_samples, times_to_run)
    #         row.append(f" %.2fms" % avg_time)
    #     rows.append(row)
    #     time.sleep(sleep_between_iteration)
    # draw_table(headers, rows)


if __name__ == '__main__':
    # parser = argparse.ArgumentParser(description='Load test biosamples bulk submission endpoint')
    # parser.add_argument('--user', type=str, required=True, help='ENA webin user')
    # parser.add_argument('--password', type=str, required=True, help='ENA webin password')
    # args = parser.parse_args()

    sample_path = './data/samples'
    user = ''
    password = ''
    no_of_samples = 1
    times_to_run = 2

    no_of_sample_list = [1, 2, 3, 4, 5, 10, 50, 100]
    times_to_run_list = [1, 5, 10, 30, 100]

    # no_of_sample_list = [1, 2]
    # times_to_run_list = [1, 5]
    sleep_between_iteration = 2

    # measure_bulk_submission_time(no_of_samples)
    # run_multiple_submissions_parallel(no_of_samples, times_to_run)
    # run_multiple_submissions_serial(no_of_samples, times_to_run)
    run(no_of_sample_list, times_to_run_list)
