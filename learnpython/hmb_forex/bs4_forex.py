#Webscarping using RealPython Tutorial 
#mentioned at https://realpython.com/python-web-scraping-practical-introduction/#making-web-requests

from requests import get
from requests.exceptions import RequestException
from contextlib import closing
from bs4 import BeautifulSoup

#Testing simple_get

test_html = get('https://www.brecorder.com/exchange-rates/', stream=True)
print(test_html)