# -*- coding: utf-8 -*-
require "cases/helper"
require 'models/owner'
require 'models/pet'
require 'models/person'

class LengthValidationTest < ActiveRecord::TestCase
  fixtures :owners

  setup do
    @owner = Class.new(Owner) do
      def self.name; 'Owner'; end
    end
  end


  def test_validates_size_of_association
    assert_nothing_raised { @owner.validates_size_of :pets, minimum: 1 }
    o = @owner.new('name' => 'nopets')
    assert !o.save
    assert o.errors[:pets].any?
    o.pets.build('name' => 'apet')
    assert o.valid?
  end

  def test_validates_size_of_association_using_within
    assert_nothing_raised { @owner.validates_size_of :pets, within: 1..2 }
    o = @owner.new('name' => 'nopets')
    assert !o.save
    assert o.errors[:pets].any?

    o.pets.build('name' => 'apet')
    assert o.valid?

    2.times { o.pets.build('name' => 'apet') }
    assert !o.save
    assert o.errors[:pets].any?
  end

  def test_validates_size_of_association_utf8
    @owner.validates_size_of :pets, minimum: 1
    o = @owner.new('name' => 'あいうえおかきくけこ')
    assert !o.save
    assert o.errors[:pets].any?
    o.pets.build('name' => 'あいうえおかきくけこ')
    assert o.valid?
  end

  def test_validates_size_of_respects_records_marked_for_destruction
    @owner.validates_size_of :pets, minimum: 1
    owner = @owner.new
    assert_not owner.save
    assert owner.errors[:pets].any?
    pet = owner.pets.build
    assert owner.valid?
    assert owner.save

    pet_count = Pet.count
    assert_not owner.update_attributes pets_attributes: [ {_destroy: 1, id: pet.id} ]
    assert_not owner.valid?
    assert owner.errors[:pets].any?
    assert_equal pet_count, Pet.count
  end

  def test_does_not_validate_length_of_if_parent_record_is_validate_false
    @owner.validates_length_of :name, minimum: 1
    owner = @owner.new
    owner.save!(validate: false)
    assert owner.persisted?

    pet = Pet.new(owner_id: owner.id)
    pet.save!

    assert_equal owner.pets.size, 1
    assert owner.valid?
    assert pet.valid?
  end
end
