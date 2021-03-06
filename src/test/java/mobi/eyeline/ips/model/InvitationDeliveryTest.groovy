package mobi.eyeline.ips.model

import static mobi.eyeline.ips.model.InvitationDelivery.State.ACTIVE
import static mobi.eyeline.ips.model.InvitationDelivery.Type.NI_DIALOG
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.hasSize

@Mixin(ValidationTestCase)
class InvitationDeliveryTest extends GroovyTestCase {

  @Override
  protected void setUp() {
    super.setUp()
    init()
  }

  void test1() {
    assertThat validate(new InvitationDelivery(state: null)), hasSize(4)
  }

  void test2() {
    def delivery = new InvitationDelivery(
        type: NI_DIALOG,
        state: ACTIVE,
        inputFile: 'txt.txt',
        speed: 1,
        text: 'a' * 200)
    def violations = validate delivery
    assertEquals 'text', violations[0].propertyPath.first().name
  }

  void test3() {
    def delivery = new InvitationDelivery(
        type: NI_DIALOG,
        state: ACTIVE,
        inputFile: 'txt.txt',
        speed: 200,
        text: 'a')
    def violations = validate delivery
    assertEquals 'speed', violations[0].propertyPath.first().name

    delivery = new InvitationDelivery(
        type: NI_DIALOG,
        state: ACTIVE,
        inputFile: 'txt.txt',
        speed: -1,
        text: 'a')
    violations = validate delivery
    assertEquals 'speed', violations[0].propertyPath.first().name
  }

  void test4() {
    def delivery = new InvitationDelivery(
        type: NI_DIALOG,
        state: ACTIVE,
        inputFile: 'txt.txt',
        speed: 50,
        text: 'a',
        retriesIntervalMinutes: 0,
        retriesMax: 0)
    def violations = validate delivery
    assertThat violations, hasSize(2)

    delivery = new InvitationDelivery(
        type: NI_DIALOG,
        state: ACTIVE,
        inputFile: 'txt.txt',
        speed: 50,
        text: 'a',
        retriesIntervalMinutes: 70,
        retriesMax: 60)
    violations = validate delivery
    assertThat violations, hasSize(2)
  }

  void test5() {
    def delivery = new InvitationDelivery(
        type: NI_DIALOG,
        state: ACTIVE,
        inputFile: 'txt.txt',
        speed: 50,
        text: 'a',
        retriesEnabled: false,
        retriesIntervalMinutes: null,
        retriesMax: null)

    def violations = validate delivery
    assertThat violations, hasSize(0)

    delivery = new InvitationDelivery(
        type: NI_DIALOG,
        state: ACTIVE,
        inputFile: 'txt.txt',
        speed: 50,
        text: 'a',
        retriesEnabled: true,
        retriesIntervalMinutes: null,
        retriesMax: null)

    violations = validate delivery
    assertThat violations, hasSize(1)

  }

}
