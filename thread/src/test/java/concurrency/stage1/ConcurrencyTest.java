package concurrency.stage1;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 스레드를 다룰 때 어떤 상황을 조심해야 할까?
 * - 상태를 가진 한 객체를 여러 스레드에서 동시에 접근할 경우
 * - static 변수를 가진 객체를 여러 스레드에서 동시에 접근할 경우
 *
 * 위 경우는 동기화(synchronization)를 적용시키거나 객체가 상태를 갖지 않도록 한다.
 * 객체를 불변 객체로 만드는 방법도 있다.
 *
 * 웹서버는 여러 사용자가 동시에 접속을 시도하기 때문에 동시성 이슈가 생길 수 있다.
 * 어떤 사례가 있는지 아래 테스트 코드를 통해 알아보자.
 */
class ConcurrencyTest {

    @Test
    void test() throws InterruptedException {
        final var userServlet = new UserServlet();

        // 웹서버로 동시에 2명의 유저가 gugu라는 이름으로 가입을 시도했다.
        // UserServlet의 users에 이미 가입된 회원이 있으면 중복 가입할 수 없도록 코드를 작성했다.
        final var firstThread = new Thread(new HttpProcessor(new User("gugu"), userServlet));
        final var secondThread = new Thread(new HttpProcessor(new User("gugu"), userServlet));

        // 스레드는 실행 순서가 정해져 있지 않다.
        // firstThread보다 늦게 시작한 secondThread가 먼저 실행될 수도 있다.
        firstThread.start();
        secondThread.start();
        secondThread.join(); // secondThread가 먼저 gugu로 가입했다.
        firstThread.join();

        // 이미 gugu로 가입한 사용자가 있어서 UserServlet.join() 메서드의 if절 조건은 false가 되고 크기는 1이다.
        // 하지만 디버거로 개별 스레드를 일시 중지하면 if절 조건이 true가 되고 크기가 2가 된다. 왜 그럴까?
        assertThat(userServlet.getUsers()).hasSize(1);

        /*
        생각 정리
        Thread의 join()메서드는 메서드 실행 이전에 위차한 명령(코드)에 대해서는 모두 실행시키도록 한다.
        즉, main thread를 비롯한 다른 Thread들은 join을 걸어둔 thread가 종료되기 전까지 기다리고 해당 쓰레드의 작업이 끝난 이후에야 이후의 코드들을 실행하게 된다.

        Debugiing breakpoint를 걸어둔 위치를 살펴보면 유저가 포함되었는지 체크하는 로직 이후 유저를 저장하는 로직에 걸었을 때 같은 user가 중복되게 저장되는
        문제가 발생하게 된다. 앞의 이론과 이러한 현상을 살펴보면 secondThread가 join이 걸린 부분은 firstThread와 secondThread가 모두 start된 이후이다.
        즉, secondThread가 종료되기 이전에 firseThread는 종료되어도 된다. 하지만 여기서 thread가 수행하는 작업에 breakpoint가 걸리다보니
        두 메서드 모두 if문 체크를 통과한 후 user가 추가하기 전 상태로 싱크가 맞춰져서 두 유저가 추가되게 되었다.

        breakpoint를 걸지 않은 경우 통과되는 이유는 secondThread가 끝나기 이전에 firstTread가 언제 실행될지는 알 수가 없어서
        둘 사이의 메서드 실행 싱크가 맞을 일은 없어서 통과되게 된다.
         */
    }
}
