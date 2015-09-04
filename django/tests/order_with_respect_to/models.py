"""
Tests for the order_with_respect_to Meta attribute.

We explicitly declare app_label on these models, because they are reused by
contenttypes_tests. When those tests are run in isolation, these models need
app_label because order_with_respect_to isn't in INSTALLED_APPS.
"""

from django.db import models
from django.utils import six
from django.utils.encoding import python_2_unicode_compatible


class Question(models.Model):
    text = models.CharField(max_length=200)

    class Meta:
        app_label = 'order_with_respect_to'


@python_2_unicode_compatible
class Answer(models.Model):
    text = models.CharField(max_length=200)
    question = models.ForeignKey(Question, models.CASCADE)

    class Meta:
        order_with_respect_to = 'question'
        app_label = 'order_with_respect_to'

    def __str__(self):
        return six.text_type(self.text)


@python_2_unicode_compatible
class Post(models.Model):
    title = models.CharField(max_length=200)
    parent = models.ForeignKey("self", models.SET_NULL, related_name="children", null=True)

    class Meta:
        order_with_respect_to = "parent"
        app_label = 'order_with_respect_to'

    def __str__(self):
        return self.title
